package com.healthyai.app.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HabitRuleEngine_Factory implements Factory<HabitRuleEngine> {
  @Override
  public HabitRuleEngine get() {
    return newInstance();
  }

  public static HabitRuleEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HabitRuleEngine newInstance() {
    return new HabitRuleEngine();
  }

  private static final class InstanceHolder {
    private static final HabitRuleEngine_Factory INSTANCE = new HabitRuleEngine_Factory();
  }
}
