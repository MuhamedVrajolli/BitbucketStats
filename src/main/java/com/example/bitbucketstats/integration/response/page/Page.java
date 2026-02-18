package com.example.bitbucketstats.integration.response.page;

import java.util.List;

public interface Page<E> {

  List<E> values();

  String next();
}
