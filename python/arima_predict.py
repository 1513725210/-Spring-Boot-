#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ARIMA 客流量趋势预测脚本

使用方式:
    python arima_predict.py <数据序列(逗号分隔)> <预测步数>

示例:
    python arima_predict.py "100,120,150,180,200,195,210" 12

输出:
    JSON 格式的预测结果，包含预测值和置信区间
"""

import sys
import json
import warnings
import os

sys.stderr = open(os.devnull, 'w')
import numpy as np

warnings.filterwarnings('ignore')


def arima_predict(data_list, steps=12):
    """
    使用 ARIMA 模型进行时间序列预测

    参数:
        data_list: 历史客流量数据列表
        steps: 预测未来的步数

    返回:
        dict: 包含预测值和置信区间的字典
    """
    try:
        from statsmodels.tsa.arima.model import ARIMA

        data = np.array(data_list, dtype=float)

        # 极速版 ARIMA 预测：跳过耗时的全网格搜索，使用固定的轻量级参数 (1, 1, 1) 或 (2, 1, 0)
        # 在实时看板中，速度通常比微小的精度提升更重要
        best_order = (1, 1, 1)
        best_aic = 0
        try:
            model = ARIMA(data, order=best_order)
            model_fit = model.fit()
        except:
            # 兼容处理：如果 (1,1,1) 无法收敛，则退化到 (1,0,0) 即简单自回归
            best_order = (1, 0, 0)
            model = ARIMA(data, order=best_order)
            model_fit = model.fit()

        # 预测未来 steps 步
        forecast = model_fit.forecast(steps=steps)
        conf_int = model_fit.get_forecast(steps=steps).conf_int(alpha=0.05)

        # 确保预测值非负
        predictions = [max(0, round(float(x))) for x in forecast]
        lower = [max(0, round(float(x))) for x in conf_int[:, 0]]
        upper = [max(0, round(float(x))) for x in conf_int[:, 1]]

        return {
            "success": True,
            "predictions": predictions,
            "confidence_lower": lower,
            "confidence_upper": upper,
            "order": list(best_order),
            "aic": round(best_aic, 2)
        }

    except ImportError:
        # 如果没有安装 statsmodels，使用简单的移动平均预测
        return simple_predict(data_list, steps)
    except Exception as e:
        return simple_predict(data_list, steps)


def simple_predict(data_list, steps=12):
    """
    简单移动平均预测（作为 ARIMA 的降级方案）
    """
    data = np.array(data_list, dtype=float)
    window = min(10, len(data))
    ma = np.mean(data[-window:])
    std = np.std(data[-window:])

    # 基于移动平均和标准差进行预测
    predictions = []
    lower = []
    upper = []

    for i in range(steps):
        # 添加轻微的趋势和随机波动
        trend = (data[-1] - data[-window]) / window if window > 1 else 0
        pred = ma + trend * (i + 1) + np.random.normal(0, std * 0.1)
        pred = max(0, round(float(pred)))
        predictions.append(pred)
        lower.append(max(0, round(float(pred - 1.96 * std))))
        upper.append(max(0, round(float(pred + 1.96 * std))))

    return {
        "success": True,
        "predictions": predictions,
        "confidence_lower": lower,
        "confidence_upper": upper,
        "order": [0, 0, 0],
        "aic": 0,
        "method": "simple_moving_average"
    }


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({
            "success": False,
            "error": "用法: python arima_predict.py <数据序列> <预测步数>"
        }))
        sys.exit(1)

    try:
        data_str = sys.argv[1]
        steps = int(sys.argv[2])

        data_list = [float(x.strip()) for x in data_str.split(',') if x.strip()]

        if len(data_list) < 10:
            print(json.dumps({
                "success": False,
                "error": "数据量不足，至少需要10个数据点"
            }))
            sys.exit(1)

        result = arima_predict(data_list, steps)
        print(json.dumps(result))

    except Exception as e:
        print(json.dumps({
            "success": False,
            "error": str(e)
        }))
        sys.exit(1)
