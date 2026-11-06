package chlorine.etjourney.content.craft;

/** It is night by the world's clock, which in the End still turns though the sky never changes. */
public final class NightCondition implements ICondition {

    @Override
    public boolean isMet(ConditionContext context) {
        return context.isNight();
    }
}
