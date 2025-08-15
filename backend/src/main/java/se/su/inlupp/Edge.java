// PROG2 VT2025, Inlämningsuppgift, del 1
// Grupp 157
// Viktor Hedman vihe4638
// Dan Zavodnik daza3914
// Axel Anderson axan8987

package se.su.inlupp;

public interface Edge<T> {

  int getWeight();

  void setWeight(int weight);

  T getDestination();

  String getName();
}
