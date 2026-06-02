package com.bank.transaction.fraud;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Builds an in-memory KieContainer from every .drl on the classpath under /rules/.
 * Hot-reloads can be wired by exposing the KieFileSystem and re-building on
 * config-server refresh — kept simple here.
 */
@Configuration
public class KieConfig {

    @Bean
    public KieContainer kieContainer() throws Exception {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        var resolver = new PathMatchingResourcePatternResolver();
        var drls = resolver.getResources("classpath*:rules/*.drl");
        for (var r : drls) {
            Resource res = ks.getResources().newClassPathResource("rules/" + r.getFilename());
            kfs.write(res);
        }

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors: " + kb.getResults());
        }
        KieRepository repo = ks.getRepository();
        return ks.newKieContainer(repo.getDefaultReleaseId());
    }
}
