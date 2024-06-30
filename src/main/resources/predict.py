import sys

def main():
    if len(sys.argv) < 3:
        print(
            "Usage: python predict.py <model_path> <monthly_savings_per_goal> <target_amount1> <target_amount2> ... <target_amountN>")
        sys.exit(1)

    model_path = sys.argv[1]
    monthly_savings_per_goal = float(sys.argv[2])
    target_amounts = [float(arg) for arg in sys.argv[3:]]

    # Debug information
    debug_info = []
    debug_info.append(f"Monthly Savings per Goal: {monthly_savings_per_goal}")
    debug_info.append(f"Target Amounts: {target_amounts}")

    predictions = []
    cumulative_savings = 0

    for target in target_amounts:
        debug_info.append(f"\nProcessing Target Amount: {target}")
        if cumulative_savings >= target:
            debug_info.append(f"Cumulative Savings {cumulative_savings} is enough to cover the target {target}.")
            predictions.append(1)  # At least 1 month needed to save for any goal
            cumulative_savings -= target
        else:
            remaining_amount = target - cumulative_savings
            debug_info.append(f"Remaining Amount to cover after using cumulative savings: {remaining_amount}")
            months_needed = remaining_amount / monthly_savings_per_goal
            predictions.append(months_needed + 1)  # Including the current month
            cumulative_savings = (months_needed - int(months_needed)) * monthly_savings_per_goal
            debug_info.append(f"Months Needed: {months_needed}, New Cumulative Savings: {cumulative_savings}")

    for prediction in predictions:
        debug_info.append(f"Prediction: {round(prediction, 2)} months")

    # Print debug information
    for info in debug_info:
        print(info)

    # Print only predictions on the last line
    print("Predictions: " + " ".join(map(str, predictions)))

if __name__ == "__main__":
    main()
