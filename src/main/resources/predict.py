import sys
import joblib
import pandas as pd

def main():
    if len(sys.argv) < 6:
        print("Usage: python predict.py <model_path> <cumulative_amount> <monthly_income> <monthly_expenses> <target_amount1> <target_amount2> ... <target_amountN>")
        sys.exit(1)

    model_path = sys.argv[1]
    cumulative_amount = float(sys.argv[2])
    monthly_income = float(sys.argv[3])
    monthly_expenses = float(sys.argv[4])
    target_amounts = [float(arg) for arg in sys.argv[5:]]

    # Calculate monthly savings
    monthly_savings = monthly_income - monthly_expenses

    # Load the model
    model = joblib.load(model_path)

    # Sort target amounts in ascending order
    target_amounts.sort()

    predictions = []

    for target in target_amounts:
        if cumulative_amount >= target:
            predictions.append(0)
        else:
            remaining_amount = target - cumulative_amount
            months_needed = remaining_amount / monthly_savings
            days_needed = months_needed * 30  # Convert months to days
            predictions.append(days_needed)

    for prediction in predictions:
        print(prediction)

if __name__ == "__main__":
    main()
