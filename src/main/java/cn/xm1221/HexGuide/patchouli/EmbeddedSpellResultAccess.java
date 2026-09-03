package cn.xm1221.HexGuide.patchouli;

/**
 * Marker mixed into Hex Casting's clientbound spell-result packet.
 *
 * The validation probe uses this interface to prove that the packet-routing
 * mixin was actually applied, rather than merely finding a valid mixin config.
 */
public interface EmbeddedSpellResultAccess {
}
