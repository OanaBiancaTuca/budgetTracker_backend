package com.example.springapp.goals;

public class PredictionResponse {
    private double averageMonthlyIncome;
    private double averageMonthlyExpenses;
    private double monthlySavings;
    private double prediction;

    public PredictionResponse(double averageMonthlyIncome, double averageMonthlyExpenses, double monthlySavings, double prediction) {
        this.averageMonthlyIncome = averageMonthlyIncome;
        this.averageMonthlyExpenses = averageMonthlyExpenses;
        this.monthlySavings = monthlySavings;
        this.prediction = prediction;
    }

    public double getAverageMonthlyIncome() {
        return averageMonthlyIncome;
    }

    public void setAverageMonthlyIncome(double averageMonthlyIncome) {
        this.averageMonthlyIncome = averageMonthlyIncome;
    }

    public double getAverageMonthlyExpenses() {
        return averageMonthlyExpenses;
    }

    public void setAverageMonthlyExpenses(double averageMonthlyExpenses) {
        this.averageMonthlyExpenses = averageMonthlyExpenses;
    }

    public double getMonthlySavings() {
        return monthlySavings;
    }

    public void setMonthlySavings(double monthlySavings) {
        this.monthlySavings = monthlySavings;
    }

    public double getPrediction() {
        return prediction;
    }

    public void setPrediction(double prediction) {
        this.prediction = prediction;
    }
}
