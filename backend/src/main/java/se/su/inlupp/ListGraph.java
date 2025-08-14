// PROG2 VT2025, Inlämningsuppgift, del 1
// Grupp 157
// Viktor Hedman vihe4638
// Dan Zavodnik daza3914
// Axel Anderson axan8987

package se.su.inlupp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class ListGraph<T> implements Graph<T> {
  
  private final Map<T, List<Edge<T>>> adjacencyList = new HashMap<>();
  
  @Override
  public void add(T node) {
    adjacencyList.putIfAbsent(node, new ArrayList<>());
  }

  @Override
  public void connect(T node1, T node2, String name, int weight) {
    validateNodesExist(node1, node2);
    
    if(weight<0){
      throw new IllegalArgumentException("Error: Vikt får inte ha negativt värde");
    }
    for(Edge<T> edge : adjacencyList.get(node1)){
      if(edge.getDestination().equals(node2)){
        throw new IllegalStateException("Error: Förbinelse mellan en eller flera noder finns redan");
      }
    }
    Edge<T> edge1 = new RouteConnection<>(node2, name, weight);
    Edge<T> edge2 = new RouteConnection<>(node1, name, weight);
    adjacencyList.get(node1).add(edge1);
    adjacencyList.get(node2).add(edge2);
  }

  @Override
  public void setConnectionWeight(T node1, T node2, int weight) {
    validateNodesExist(node1, node2);

    if(weight < 0 ){
      throw new IllegalArgumentException("Error: Vikt får inte ha negativt värde");
    }
    Edge<T> edge1 = getEdgeBetween(node1, node2);
    Edge<T> edge2 = getEdgeBetween(node2, node1);
    if (edge1 == null || edge2 == null) {
      throw new NoSuchElementException("Error: Kant saknas mellan noderna");
    }
    edge1.setWeight(weight);
    edge2.setWeight(weight); 
  }

  @Override
  public Set<T> getNodes() {
    return new HashSet<>(adjacencyList.keySet());
  }

  @Override
  public Collection<Edge<T>> getEdgesFrom(T node) {
    validateNodesExist(node);

    return Collections.unmodifiableList(adjacencyList.get(node));
  }

  @Override
  public Edge<T> getEdgeBetween(T node1, T node2) {
    validateNodesExist(node1, node2);
    for(Edge<T> edge : adjacencyList.get(node1)){
      if(edge.getDestination().equals(node2)){
        return edge;
      }
    }
    return null;
  }

  @Override
  public void disconnect(T node1, T node2) {
    validateNodesExist(node1, node2);
    Edge<T> edge1 = getEdgeBetween(node1, node2);
    Edge<T> edge2 = getEdgeBetween(node2, node1);
    if(edge1 == null || edge2 == null){
      throw new IllegalStateException("Error: Ingen kant mellan noderna");
    }
    adjacencyList.get(node1).remove(edge1);
    adjacencyList.get(node2).remove(edge2);
    
  }

  @Override 
  public void remove(T node) {
    validateNodesExist(node);

    for(T other : adjacencyList.keySet()){
      if(!other.equals(node)){
        adjacencyList.get(other).removeIf(edge -> edge.getDestination().equals(node));
      }
    }
    adjacencyList.remove(node);
  }

  @Override
public String toString() {
   StringBuilder sb = new StringBuilder();
    for (T node : adjacencyList.keySet()) {
        sb.append(node.toString()).append(":\n");
        for (Edge<T> edge : adjacencyList.get(node)) {
            sb.append("  ").append(edge.toString()).append("\n");
        }
    }
    return sb.toString();
}

  @Override
  public boolean pathExists(T from, T to) {
    return getPath(from, to) != null;
  }

  @Override
  public List<Edge<T>> getPath(T from, T to) {
    if(!adjacencyList.containsKey(from) || !adjacencyList.containsKey(to)){
      return null;
    }
    return dfsFindPath(from, to, new HashSet<>());
  }

  private List<Edge<T>> dfsFindPath(T current, T target, Set<T> visited){
    if(current.equals(target)){
      return new ArrayList<>();
    }
    visited.add(current);

    for(Edge<T> edge : adjacencyList.get(current)){
      T neighbor = edge.getDestination();
      if(!visited.contains(neighbor)){
        List<Edge<T>> path = dfsFindPath(neighbor, target, visited);
        if(path!=null){
          path.add(0,edge);
          return path;
        }
      }
    }
    return null;
  }

  private void validateNodesExist(T... nodes) {
    for (T node : nodes) {
      if (!adjacencyList.containsKey(node)) {
        throw new NoSuchElementException("Error: Nod saknas i grafen: " + node);
      }
    }
  }
}
