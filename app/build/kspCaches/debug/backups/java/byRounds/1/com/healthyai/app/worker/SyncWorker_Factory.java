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
public final class SyncWorker_Factory {
  public SyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams);
  }

  public static SyncWorker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SyncWorker newInstance(Context context, WorkerParameters workerParams) {
    return new SyncWorker(context, workerParams);
  }

  private static final class InstanceHolder {
    private static final SyncWorker_Factory INSTANCE = new SyncWorker_Factory();
  }
}
