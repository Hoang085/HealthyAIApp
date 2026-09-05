package com.healthyai.app.presentation.screen.home;

import com.healthyai.app.domain.repository.HealthDataRepository;
import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider;

  private final Provider<HealthDataRepository> healthDataRepositoryProvider;

  public HomeViewModel_Factory(Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<HealthDataRepository> healthDataRepositoryProvider) {
    this.getTodayHabitsUseCaseProvider = getTodayHabitsUseCaseProvider;
    this.healthDataRepositoryProvider = healthDataRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getTodayHabitsUseCaseProvider.get(), healthDataRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<HealthDataRepository> healthDataRepositoryProvider) {
    return new HomeViewModel_Factory(getTodayHabitsUseCaseProvider, healthDataRepositoryProvider);
  }

  public static HomeViewModel newInstance(GetTodayHabitsUseCase getTodayHabitsUseCase,
      HealthDataRepository healthDataRepository) {
    return new HomeViewModel(getTodayHabitsUseCase, healthDataRepository);
  }
}
