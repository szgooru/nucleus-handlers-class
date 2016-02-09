package org.gooru.nucleus.handlers.classes.processors.repositories.generators;

import java.security.SecureRandom;

/**
 * Created by ashish on 9/2/16.
 */
class ClassCodeGenerator implements Generator<String> {
  @Override
  public String generate() {
    return RandomClassIdStringGenerator.generateClassId();
  }

  ClassCodeGenerator() {
  }

  private static final class RandomClassIdStringGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final char[] CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 7;

    public static String generateClassId() {
      int count = LENGTH;
      int start = 0, end = CHARACTERS.length;
      char[] buffer = new char[count];
      int gap = end - start;

      while (count-- != 0) {
        char ch;
        ch = CHARACTERS[secureRandom.nextInt(gap) + start];
        buffer[count] = ch;
      }
      return new String(buffer);
    }

    private RandomClassIdStringGenerator() {
      throw new AssertionError();
    }
  }


}
