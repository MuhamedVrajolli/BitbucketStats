package com.example.bitbucketstats.models.page;

import com.example.bitbucketstats.models.bitbucket.DiffStat;
import java.util.List;

public record DiffStatPage(String next, List<DiffStat> values) implements Page<DiffStat> {

}
