package com.healthyai.app.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class ReminderWorker_Factory {
  public ReminderWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams);
  }

  public static ReminderWorker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ReminderWorker newInstance(Context context, WorkerParameters workerParams) {
    return new ReminderWorker(context, workerParams);
  }

  private static final class InstanceHolder {
    private static final ReminderWorker_Factory INSTANCE = new ReminderWorker_Factory();
  }
}
