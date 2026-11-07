package chlorine.etjourney.content.craft;

/** Something about the place that must hold for a recipe to run. */
public interface ICondition {

    boolean isMet(ConditionContext context);
}
