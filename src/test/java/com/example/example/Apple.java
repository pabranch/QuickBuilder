package com.example.example;

public class Apple extends Fruit {
  private int leaves;

  public Apple(int leaves) {
    this.leaves = leaves;
  }

  public int numberOfLeaves() {
    return leaves;
  }
}
