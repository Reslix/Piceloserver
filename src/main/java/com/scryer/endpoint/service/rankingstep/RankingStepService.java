package com.scryer.endpoint.service.rankingstep;

import com.scryer.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

@Service
public class RankingStepService {
    private final RankingStepDBAdapter rankingStepDBAdapter;

    @Autowired
    public RankingStepService(final RankingStepDBAdapter rankingStepDBAdapter) {
        this.rankingStepDBAdapter = rankingStepDBAdapter;
    }

    public Mono<List<RankingStep>> createRankingSteps(final List<RankingStep> rankingSteps) {
        var uniqueIdsMono = rankingStepDBAdapter.getUniqueId().repeat(rankingSteps.size()).collectList();
        return uniqueIdsMono.map(ids -> {
                    var newSteps = new ArrayList<RankingStep>(rankingSteps.size());
                    newSteps.add(RankingStep.builder().id(ids.get(0))
                                         .userId(rankingSteps.get(0).getUserId())
                                         .imageRankingId(rankingSteps.get(0).getImageRankingId())
                                         .name(rankingSteps.get(0).getName())
                                         .source(rankingSteps.get(0).getSource())
                                         .target(rankingSteps.get(0).getTarget())
                                         .meta(rankingSteps.get(0).getMeta()).build());

                    for (int i = 1; i < rankingSteps.size(); i++) {
                        newSteps.add(RankingStep.builder()
                                             .id(ids.get(i))
                                             .userId(rankingSteps.get(i).getUserId())
                                             .imageRankingId(rankingSteps.get(i).getImageRankingId())
                                             .name(rankingSteps.get(i).getName())
                                             .source(newSteps.get(i - 1).getId())
                                             .target(rankingSteps.get(i).getTarget())
                                             .meta(rankingSteps.get(i).getMeta())
                                             .build());
                    }
                    return newSteps;
                })
                .flatMapIterable(steps -> steps)
                .flatMap(rankingStepDBAdapter::createRankingStep)
                .collectList();
    }

    public Flux<RankingStep> getRankingStepsByImageRankingId(final String id) {
        return rankingStepDBAdapter.getRankingStepsByImageRankingId(id);
    }

    public Mono<RankingStep> updateRankingStep(final RankingStep rankingStep) {
        return rankingStepDBAdapter.updateRankingStep(rankingStep);
    }

    public Mono<RankingStep> deleteRankingStep(final RankingStep rankingStep) {
        return rankingStepDBAdapter.deleteRankingStep(rankingStep);
    }
}
