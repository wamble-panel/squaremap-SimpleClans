package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

/** A clan's protected land on one world: how many claims and how many blocks they cover. */
public record Territory(int claims, long blocks) {

    public Territory plus(Territory other) {
        return new Territory(claims + other.claims, blocks + other.blocks);
    }
}
