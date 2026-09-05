package com.healthyai.app.presentation.screen.habits;

import com.healthyai.app.domain.usecase.GetTodayHabitsUseCase;
import com.healthyai.app.domain.usecase.LogHabitUseCase;
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
public final class HabitsViewModel_Factory implements Factory<HabitsViewModel> {
  private final Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider;

  private final Provider<LogHabitUseCase> logHabitUseCaseProvider;

  public HabitsViewModel_Factory(Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<LogHabitUseCase> logHabitUseCaseProvider) {
    this.getTodayHabitsUseCaseProvider = getTodayHabitsUseCaseProvider;
    this.logHabitUseCaseProvider = logHabitUseCaseProvider;
  }

  @Override
  public HabitsViewModel get() {
    return newInstance(getTodayHabitsUseCaseProvider.get(), logHabitUseCaseProvider.get());
  }

  public static HabitsViewModel_Factory create(
      Provider<GetTodayHabitsUseCase> getTodayHabitsUseCaseProvider,
      Provider<LogHabitUseCase> logHabitUseCaseProvider) {
    return new HabitsViewModel_Factory(getTodayHabitsUseCaseProvider, logHabitUseCaseProvider);
  }

  public static HabitsViewModel newInstance(GetTodayHabitsUseCase getTodayHabitsUseCase,
      LogHabitUseCase logHabitUseCase) {
    return new HabitsViewModel(getTodayHabitsUseCase, logHabitUseCase);
  }
}
