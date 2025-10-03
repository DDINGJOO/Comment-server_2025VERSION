package com.teambind.commentserver.utils.primarykey;

public interface KeyProvider {
  String generateKey();

  Long generateLongKey();
}
