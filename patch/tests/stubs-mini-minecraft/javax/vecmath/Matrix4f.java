package javax.vecmath;
public class Matrix4f {
    public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;
    public Matrix4f(float[] v) { m00 = v[0]; m01 = v[1]; m02 = v[2]; m03 = v[3]; m10 = v[4]; m11 = v[5]; m12 = v[6]; m13 = v[7]; m20 = v[8]; m21 = v[9]; m22 = v[10]; m23 = v[11]; m30 = v[12]; m31 = v[13]; m32 = v[14]; m33 = v[15]; }
}
