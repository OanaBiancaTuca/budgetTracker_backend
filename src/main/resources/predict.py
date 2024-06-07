import sys
import joblib
import pandas as pd

def main():
    if len(sys.argv) < 4:
        print("Usage: python predict.py <model_path> <cumulative_amount> <target_amount1> <target_amount2> ... <target_amountN>")
        sys.exit(1)

    model_path = sys.argv[1]
    cumulative_amount = float(sys.argv[2])
    target_amounts = [float(arg) for arg in sys.argv[3:]]

    model = joblib.load(model_path)

    # Create a DataFrame for the input
    input_data = pd.DataFrame({
        'cumulative_amount': [cumulative_amount] * len(target_amounts),
        'target_amount': target_amounts
    })

    # Predict
    predictions = model.predict(input_data)

    for prediction in predictions:
        print(prediction)

if __name__ == "__main__":
    main()
