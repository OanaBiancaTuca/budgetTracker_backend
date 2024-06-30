import joblib
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error
from sklearn.model_selection import train_test_split

# Set de date exemplu cu o abordare mai realistă
data = {
    'cumulative_amount': [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000],
    'target_amount': [50, 100, 150, 200, 250, 300, 350, 400, 450, 500],
    'monthly_savings': [50, 60, 70, 80, 90, 100, 110, 120, 130, 140],
    'months_to_achieve': [1, 2, 2, 3, 3, 3, 4, 4, 4, 4]
}

df = pd.DataFrame(data)

# Caracteristici și țintă
X = df[['cumulative_amount', 'target_amount', 'monthly_savings']]
y = df['months_to_achieve']

# Împarte datele în seturi de antrenament și testare
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Antrenează modelul
model = LinearRegression()
model.fit(X_train, y_train)

# Evaluează modelul
predictions = model.predict(X_test)
mse = mean_squared_error(y_test, predictions)
print(f'Mean Squared Error: {mse}')

# Salvează modelul
joblib.dump(model, 'goal_prediction_model.pkl')
