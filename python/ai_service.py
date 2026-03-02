import os
import re
import traceback
import pymysql
from flask import Flask, request, jsonify
from flask_cors import CORS
from openai import OpenAI

app = Flask(__name__)
CORS(app)

# DeepSeek API Configuration
API_KEY = "sk-621fe659f0f94ab5893e96f537822cc7"
BASE_URL = "https://api.deepseek.com"

# Initialize OpenAI client with DeepSeek settings
client = OpenAI(api_key=API_KEY, base_url=BASE_URL)

def get_chat_completion(messages, temperature=0.7):
    try:
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=messages,
            temperature=temperature,
            stream=False
        )
        return response.choices[0].message.content
    except Exception as e:
        print(f"Error calling DeepSeek API: {e}")
        traceback.print_exc()
        raise

@app.route('/api/ai/sql_generation', methods=['POST'])
def sql_generation():
    try:
        data = request.json
        user_query = data.get('query')
        schema_prompt = data.get('schema_prompt')
        
        if not user_query or not schema_prompt:
            return jsonify({"success": False, "error": "Missing query or schema_prompt"}), 400

        messages = [
            {"role": "system", "content": schema_prompt},
            {"role": "user", "content": "用户问题: " + user_query}
        ]
        
        ai_response = get_chat_completion(messages, temperature=0.1)
        
        # Extract SQL from markdown
        sql = ai_response
        match = re.search(r'```sql(.*?)```', ai_response, re.DOTALL)
        if match:
            sql = match.group(1).strip()
        elif ai_response.strip().upper().startswith("SELECT"):
            sql = ai_response.strip()
        
        return jsonify({
            "success": True,
            "sql": sql,
            "raw_response": ai_response
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

db_config = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "tourism_warning",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}

SCHEMA_PROMPT = """你是一个景区数据管家（AI Agent），你的任务是将用户的自然语言查询转换为 MySQL 查询语句（Text-to-SQL），并基于查询结果回答用户问题。

【数据库表结构说明】
1. 表 `scenic_spot` (景区信息表)
   字段: id(主键), name(景区名称), city(城市), max_capacity(最大承载量), current_count(当前人数), level(等级), status(状态 1开放 0关闭)
2. 表 `flow_record` (客流历史流水表)
   字段: id, scenic_id(景区ID), current_count(记录时的客流人数), in_count(进入人数), out_count(离开人数), congestion_rate(拥挤度%), record_time(记录时间)
3. 表 `warning_log` (预警记录表)
   字段: id, scenic_id, scenic_name(景区名), warning_level(YELLOW/RED), current_count, max_capacity, threshold_percent, congestion_rate, message, warning_time

【交互规则】
你需要输出合法的 SQL 语句来查询数据，必须用 ```sql 和 ``` 包裹 SQL 语句。
只能包含一条 SELECT 查询，绝对禁止 DELETE/UPDATE/INSERT 等修改操作。
如果用户的问题需要查询数据库，请只返回 ```sql...```，不需要解释说明。"""

@app.route('/api/ai/chat', methods=['POST'])
def chat_endpoint():
    try:
        data = request.json
        user_query = data.get('query')

        # Step 1: Text-to-SQL
        messages = [
            {"role": "system", "content": SCHEMA_PROMPT},
            {"role": "user", "content": "用户问题: " + user_query}
        ]
        
        ai_response = get_chat_completion(messages, temperature=0.1)
        
        sql = ai_response
        match = re.search(r'```sql(.*?)```', ai_response, re.DOTALL)
        if match:
            sql = match.group(1).strip()
        elif ai_response.strip().upper().startswith("SELECT"):
            sql = ai_response.strip()

        if not sql or not sql.lower().startswith('select') or 'update ' in sql.lower() or 'delete ' in sql.lower():
            # If no SELECT SQL is found, fallback to conversational mode
            return jsonify({
                "code": 200,
                "data": get_chat_completion([{"role": "system", "content": "你是景区数据管家助理。由于种种原因未能查询数据库，请礼貌地回答用户。"}, {"role": "user", "content": user_query}])
            })

        # Step 2: Execute SQL
        connection = pymysql.connect(**db_config)
        with connection.cursor() as cursor:
            cursor.execute(sql)
            result = cursor.fetchall()
        connection.close()
        
        db_result_str = str(result)[:2000] # Truncate if too long
        
        # Step 3: Summarize
        summary_prompt = (
            "你是专业的数据分析师。请根据用户的问题以及从数据库查询出的原生数据，"
            "用通俗易懂、专业的语言给出分析报告或回答。\n"
            f"用户问题: {user_query}\n"
            f"数据库查询结果: {db_result_str}"
        )

        final_answer = get_chat_completion([
            {"role": "system", "content": "请提供友好的景区数据分析服务。"},
            {"role": "user", "content": summary_prompt}
        ], temperature=0.5)

        return jsonify({
            "code": 200, # Matches frontend expectation
            "data": final_answer
        })
    except Exception as e:
        traceback.print_exc()
        return jsonify({"code": 500, "message": str(e)})

@app.route('/api/ai/data_summary', methods=['POST'])
def data_summary():
    try:
        data = request.json
        user_query = data.get('query')
        db_result = data.get('db_result')

        prompt = (
            "你是专业的数据分析师。请根据用户的问题以及从数据库查询出的原生数据，"
            "用通俗易懂、专业的语言给出分析报告或回答。\n"
            f"用户问题: {user_query}\n"
            f"数据库查询结果: {db_result}"
        )

        messages = [
            {"role": "system", "content": "请提供友好的景区数据分析服务。"},
            {"role": "user", "content": prompt}
        ]

        summary = get_chat_completion(messages, temperature=0.5)
        
        return jsonify({
            "success": True,
            "summary": summary
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/generate_plan', methods=['POST'])
def generate_plan():
    try:
        data = request.json
        scenic_name = data.get('scenic_name')
        level = data.get('level')
        current_count = data.get('current_count')
        congestion_rate = data.get('congestion_rate')
        sop_context = data.get('sop_context')

        prompt = (
            "你是景区安全指挥中心的高级决策助手。当景区发生拥挤预警时，你的任务是根据预警级别和现场情况，"
            "结合给定的《应急预案知识库(SOP)》，生成一份条理清晰、可以直接执行的3-4步应急指令，提供给现场管理人员。\n\n"
            f"【知识库内容】:\n{sop_context}\n\n"
            "请根据以上知识库，为以下紧急情况生成处置指南，要求使用 HTML 格式（方便前端渲染），包含 `<ul>` 和 `<li>`。"
            "不要输出其他的寒暄语等。"
        )

        level_str = "红色预警(严重拥挤)" if level == "RED" else "黄色预警(客流高峰)"
        user_query = (
            f"当前发生预警！\n地点: {scenic_name}\n预警级别: {level_str}\n"
            f"当前人数: {current_count}\n拥挤度: {congestion_rate}%\n"
            "请立即生成针对该情况的定向处置步骤。"
        )

        messages = [
            {"role": "system", "content": prompt},
            {"role": "user", "content": user_query}
        ]

        plan = get_chat_completion(messages, temperature=0.4)
        
        return jsonify({
            "success": True,
            "plan": plan
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/sentiment_analysis', methods=['POST'])
def sentiment_analysis():
    try:
        data = request.json
        reviews = data.get('reviews')

        prompt = (
            "你是景区舆情分析专家。我将给你几条针对某景区的最新游客评论。\n"
            "请进行情感分析，判断是否出现【严重的多人聚集、退票、打架、挤死了】等负面情绪。\n"
            "只需要回答：【正常】或者【舆情预警】，并简要说明提取到的关键标签（如：挤死了、退票）。\n"
            "不要输出多余解释。"
        )

        messages = [
            {"role": "system", "content": prompt},
            {"role": "user", "content": reviews}
        ]

        analysis = get_chat_completion(messages, temperature=0.2)
        
        return jsonify({
            "success": True,
            "analysis": analysis
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

if __name__ == '__main__':
    # Run on port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)
