package dk.stream.android;

import android.app.job.*;
import android.content.*;
import android.os.*;
import java.util.concurrent.*;

/** A persisted, network-constrained daily job; updates one guide at a time. */
public final class DailyGuideJob extends JobService {
 static final String CHANGED="dk.stream.android.GUIDES_UPDATED";
 private static final int ID=24001;
 static final long DAY=TimeUnit.DAYS.toMillis(1);
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private Future<?> task;private EpgRepository repository;private java.util.concurrent.atomic.AtomicBoolean cancellation;
 static void sync(Context context,Library library){try{JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler==null)return;boolean exists=false;for(JobInfo job:scheduler.getAllPendingJobs())if(job.getId()==ID)exists=true;if(!library.dailyGuideUpdate||library.guides.isEmpty()){if(exists)scheduler.cancel(ID);return;}if(!exists){JobInfo job=new JobInfo.Builder(ID,new ComponentName(context,DailyGuideJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(DAY).setPersisted(true).build();if(scheduler.schedule(job)!=JobScheduler.RESULT_SUCCESS)android.util.Log.e("StreamGuideUpdate","Daily guide job could not be scheduled");}}catch(RuntimeException unavailable){android.util.Log.e("StreamGuideUpdate","Daily guide job unavailable",unavailable);}}
 @Override public boolean onStartJob(JobParameters parameters){repository=new EpgRepository(this);final EpgRepository cache=repository;final java.util.concurrent.atomic.AtomicBoolean stopped=new java.util.concurrent.atomic.AtomicBoolean();cancellation=stopped;task=worker.submit(()->{boolean changed=false;try{Library snapshot=new LibraryStore(this).load();if(snapshot.dailyGuideUpdate)for(Library.Guide guide:snapshot.guides){if(stopped.get()||Thread.currentThread().isInterrupted())return;Library current=new LibraryStore(this).load();Library.Guide selected=current.findGuide(guide.id);if(!current.dailyGuideUpdate)break;if(selected==null||!selected.url.equals(guide.url))continue;long now=System.currentTimeMillis();if(!cache.dailyDue(guide,now))continue;try{cache.fetch(guide,now);changed=true;}catch(java.io.IOException failed){if(stopped.get()||Thread.currentThread().isInterrupted())return;android.util.Log.w("StreamGuideUpdate","A guide update failed; cached data retained");}}}catch(Exception unavailable){android.util.Log.w("StreamGuideUpdate","Could not read daily guide settings");}finally{if(!stopped.get()&&!Thread.currentThread().isInterrupted()){boolean updated=changed;main.post(()->{if(stopped.get())return;if(updated)sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));jobFinished(parameters,false);});}}});return true;}
 @Override public boolean onStopJob(JobParameters parameters){if(cancellation!=null)cancellation.set(true);if(task!=null)task.cancel(true);if(repository!=null)repository.cancel();return true;}
 @Override public void onDestroy(){if(cancellation!=null)cancellation.set(true);if(task!=null)task.cancel(true);if(repository!=null)repository.cancel();worker.shutdownNow();super.onDestroy();}
}
