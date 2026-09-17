package com.healthyai.app.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.healthyai.app.domain.repository.HealthDataRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HealthSyncWorker_Factory {
  private final Provider<HealthDataRepository> healthDataRepositoryProvider;

  public HealthSyncWorker_Factory(Provider<HealthDataRepository> healthDataRepositoryProvider) {
    this.healthDataRepositoryProvider = healthDataRepositoryProvider;
  }

  public HealthSyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, healthDataRepositoryProvider.get());
  }

  public static HealthSyncWorker_Factory create(
      Provider<HealthDataRepository> healthDataRepositoryProvider) {
    return new HealthSyncWorker_Factory(healthDataRepositoryProvider);
  }

  public static HealthSyncWorker newInstance(Context context, WorkerParameters workerParams,
      HealthDataRepository healthDataRepository) {
    return new HealthSyncWorker(context, workerParams, healthDataRepository);
  }
}
