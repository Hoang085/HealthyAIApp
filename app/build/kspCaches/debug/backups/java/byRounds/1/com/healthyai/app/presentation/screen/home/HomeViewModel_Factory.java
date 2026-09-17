package com.healthyai.app.presentation.screen.home;

import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase;
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase;
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

  private final Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider;

  public HomeViewModel_Factory(Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider) {
    this.getTodayHabitsUseCaseProvider = getTodayHabitsUseCaseProvider;
    this.getHealthDataUseCaseProvider = getHealthDataUseCaseProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getTodayHabitsUseCaseProvider.get(), getHealthDataUseCaseProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider) {
    return new HomeViewModel_Factory(getTodayHabitsUseCaseProvider, getHealthDataUseCaseProvider);
  }

  public static HomeViewModel newInstance(GetTodayHabitsUseCase getTodayHabitsUseCase,
      GetHealthDataUseCase getHealthDataUseCase) {
    return new HomeViewModel(getTodayHabitsUseCase, getHealthDataUseCase);
  }
}
