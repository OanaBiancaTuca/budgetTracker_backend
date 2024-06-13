import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error
import joblib

# Example dataset
data = {
    'cumulative_amount': [100, 200, 300, 400, 500],
    'target_amount': [50, 100, 150, 200, 250],
    'months_to_achieve': [2, 4, 6, 8, 10]
}

df = pd.DataFrame(data)

# Features and target
X = df[['cumulative_amount', 'target_amount']]
y = df['months_to_achieve']

# Împarte datele în seturi de antrenament și testare
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Antrenează modelul
model = LinearRegression()
model.fit(X_train, y_train)

# Evaluate the model
predictions = model.predict(X_test)
mse = mean_squared_error(y_test, predictions)
print(f'Mean Squared Error: {mse}')

# Salveaza modelul
joblib.dump(model, 'goal_prediction_model.pkl')
