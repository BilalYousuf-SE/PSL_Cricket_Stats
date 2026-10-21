package com.cricket.stats.repository;

import com.cricket.stats.entity.Match;
import com.cricket.stats.entity.MatchSquad;
import com.cricket.stats.entity.Partnership;
import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.Season;
import com.cricket.stats.entity.Team;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class RepositoryQueriesTest {
    @Test
    void squadOrderingAndPartnershipQueriesResolveAgainstTheEntityMappings() {
        // Validate JPQL with Hibernate itself; no connection or database is needed.
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", "false")
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        try (SessionFactory factory = new MetadataSources(registry)
                .addAnnotatedClass(Match.class).addAnnotatedClass(MatchSquad.class)
                .addAnnotatedClass(Partnership.class).addAnnotatedClass(Player.class)
                .addAnnotatedClass(Season.class).addAnnotatedClass(Team.class)
                .buildMetadata().buildSessionFactory();
             var session = factory.openSession()) {
            for (Class<?> repository : new Class<?>[]{MatchSquadRepository.class, PartnershipRepository.class}) {
                for (var method : repository.getDeclaredMethods()) {
                    Query query = method.getAnnotation(Query.class);
                    if (query != null) session.createQuery(query.value(), Object.class);
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
