package com.teambind.commentserver.utils.primarykey;

public interface PrimaryKeyProvider {
  String generateKey();

  Long generateLongKey();
}
