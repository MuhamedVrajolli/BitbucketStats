package com.example.bitbucketstats.models.page;

import com.example.bitbucketstats.integration.response.DiffStat;
import java.util.List;

public record DiffStatPage(String next, List<DiffStat> values) implements Page<DiffStat> {

}
