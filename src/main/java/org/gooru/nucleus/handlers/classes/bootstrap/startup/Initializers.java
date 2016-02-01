package org.gooru.nucleus.handlers.classes.bootstrap.startup;

import org.gooru.nucleus.handlers.classes.app.components.DataSourceRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Initializers implements Iterable<Initializer> {


  private List<Initializer> initializers = null;
  private Iterator<Initializer> internalIterator;

  public Initializers() {
    initializers = new ArrayList<Initializer>();
    initializers.add(DataSourceRegistry.getInstance());
    internalIterator = initializers.iterator();
  }

  @Override
  public Iterator<Initializer> iterator() {
    Iterator<Initializer> iterator = new Iterator<Initializer>() {

      @Override
      public boolean hasNext() {
        return internalIterator.hasNext();
      }

      @Override
      public Initializer next() {
        return internalIterator.next();
      }

    };
    return iterator;
  }


}