package io.kalyansikdar.ipldashboard.data;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.kalyansikdar.ipldashboard.model.Team;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

  private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

  private final EntityManager entityManager;

  @Autowired
  public JobCompletionNotificationListener(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public void afterJob(JobExecution jobExecution) {
    if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("!!! JOB FINISHED! Time to verify the results");

      Map<String, Team> teamData = new HashMap<>();

      entityManager.createQuery("select team1, count(*) from Match group by team1", Object[].class)
      .getResultList()
      .stream()
      .map(entity -> new Team((String) entity[0], (long) entity[1]))
      .forEach(team -> teamData.put(team.getTeamName(), team));

      entityManager.createQuery("select team2, count(*) from Match group by team2", Object[].class)
      .getResultList()
      .stream()
      .forEach(entity -> {
          Team team = teamData.get((String) entity[0]);
          team.setTotalMatches(team.getTotalMatches() + (long) entity[1]);
      });
      
      entityManager.createQuery("select matchWinner, count(*) from Match group by matchWinner ", Object[].class)
      .getResultList()
      .stream()
      .forEach(entity -> {
          Team team = teamData.get((String) entity[0]);
          if (team != null) {
            team.setTotalWins((long) entity[1]);
          }
      });

      teamData.values().forEach(team -> entityManager.persist(team));
      teamData.values().forEach(team -> System.out.println(team));

    }
  }
}