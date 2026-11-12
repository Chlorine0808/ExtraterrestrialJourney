package chlorine.etjourney.core.combat.model;

/** A part that says whether a hook or boost applies now. */
public interface Condition {

    boolean test(Situation situation);
}
