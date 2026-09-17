package com.healthyai.app.domain.usecase.health;

import com.healthyai.app.domain.repository.HabitRepository;
import com.healthyai.app.domain.usecase.HabitRuleEngine;
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
public final class GetVitalityScoreUseCase_Factory implements Factory<GetVitalityScoreUseCase> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  private final Provider<HabitRuleEngine> ruleEngineProvider;

  public GetVitalityScoreUseCase_Factory(Provider<HabitRepository> habitRepositoryProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
    this.ruleEngineProvider = ruleEngineProvider;
  }

  @Override
  public GetVitalityScoreUseCase get() {
    return newInstance(habitRepositoryProvider.get(), ruleEngineProvider.get());
  }

  public static GetVitalityScoreUseCase_Factory create(
      Provider<HabitRepository> habitRepositoryProvider,
      Provider<HabitRuleEngine> ruleEngineProvider) {
    return new GetVitalityScoreUseCase_Factory(habitRepositoryProvider, ruleEngineProvider);
  }

  public static GetVitalityScoreUseCase newInstance(HabitRepository habitRepository,
      HabitRuleEngine ruleEngine) {
    return new GetVitalityScoreUseCase(habitRepository, ruleEngine);
  }
}
