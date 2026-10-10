package net.minecraft.client.renderer.block.model;
public interface IBakedModel {
    default org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(ItemCameraTransforms.TransformType t) { return org.apache.commons.lang3.tuple.Pair.of(this, null); }
}
