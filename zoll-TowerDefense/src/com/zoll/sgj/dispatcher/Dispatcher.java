package com.zoll.sgj.dispatcher;

import java.util.ArrayList;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class Dispatcher implements Job{
	private List<ITick> tickable = new ArrayList<ITick>();

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		System.out.println(arg0);
		for (ITick eacheTickable : tickable) {
			eacheTickable.onTick();
		}
	}
	
	public static void main(String[] args) throws SchedulerException {
		JobDetail job = JobBuilder.newJob().ofType(Dispatcher.class).usingJobData("dataKey", "value").withIdentity("name", "group").build();
		CronTrigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule("0/2 * * * * ? *")).forJob("name", "group").build();
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler scheduler = sf.getScheduler();
		scheduler.scheduleJob(job, trigger);
		scheduler.start();
	}
}
