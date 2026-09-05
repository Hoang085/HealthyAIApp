package com.healthyai.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HiltApplication_MembersInjector implements MembersInjector<HiltApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public HiltApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<HiltApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HiltApplication_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(HiltApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.healthyai.app.HiltApplication.workerFactory")
  public static void injectWorkerFactory(HiltApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
