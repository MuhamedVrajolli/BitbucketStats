package com.example.bitbucketstats.models;

import java.util.List;

public record DiffStatPage(String next, List<DiffStat> values) implements NextPage {

}
