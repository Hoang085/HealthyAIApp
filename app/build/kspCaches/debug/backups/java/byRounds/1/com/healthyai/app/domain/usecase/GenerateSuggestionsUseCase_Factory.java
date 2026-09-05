package com.healthyai.app.domain.usecase;

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
public final class GenerateSuggestionsUseCase_Factory implements Factory<GenerateSuggestionsUseCase> {
  private final Provider<HabitRuleEngine> ruleEngineProvider;

  public GenerateSuggestionsUseCase_Factory(Provider<HabitRuleEngine> ruleEngineProvider) {
    this.ruleEngineProvider = ruleEngineProvider;
  }

  @Override
  public GenerateSuggestionsUseCase get() {
    return newInstance(ruleEngineProvider.get());
  }

  public static GenerateSuggestionsUseCase_Factory create(
      Provider<HabitRuleEngine> ruleEngineProvider) {
    return new GenerateSuggestionsUseCase_Factory(ruleEngineProvider);
  }

  public static GenerateSuggestionsUseCase newInstance(HabitRuleEngine ruleEngine) {
    return new GenerateSuggestionsUseCase(ruleEngine);
  }
}
