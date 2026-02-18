package com.example.bitbucketstats.models.page;

import java.util.List;

public interface Page<E> {

  List<E> values();

  String next();
}
