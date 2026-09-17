package com.healthyai.app.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.healthyai.app.data.firebase.FirestoreSyncDataSource;
import com.healthyai.app.domain.repository.HabitRepository;
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
public final class SyncWorker_Factory {
  private final Provider<HabitRepository> habitRepositoryProvider;

  private final Provider<FirestoreSyncDataSource> firestoreSyncDataSourceProvider;

  private final Provider<FirebaseAuth> authProvider;

  public SyncWorker_Factory(Provider<HabitRepository> habitRepositoryProvider,
      Provider<FirestoreSyncDataSource> firestoreSyncDataSourceProvider,
      Provider<FirebaseAuth> authProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
    this.firestoreSyncDataSourceProvider = firestoreSyncDataSourceProvider;
    this.authProvider = authProvider;
  }

  public SyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, habitRepositoryProvider.get(), firestoreSyncDataSourceProvider.get(), authProvider.get());
  }

  public static SyncWorker_Factory create(Provider<HabitRepository> habitRepositoryProvider,
      Provider<FirestoreSyncDataSource> firestoreSyncDataSourceProvider,
      Provider<FirebaseAuth> authProvider) {
    return new SyncWorker_Factory(habitRepositoryProvider, firestoreSyncDataSourceProvider, authProvider);
  }

  public static SyncWorker newInstance(Context context, WorkerParameters workerParams,
      HabitRepository habitRepository, FirestoreSyncDataSource firestoreSyncDataSource,
      FirebaseAuth auth) {
    return new SyncWorker(context, workerParams, habitRepository, firestoreSyncDataSource, auth);
  }
}
