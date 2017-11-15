package android.org.rajawali3d.postprocessing.effects;

import android.org.rajawali3d.cameras.Camera;
import android.org.rajawali3d.lights.DirectionalLight;
import android.org.rajawali3d.materials.textures.ATexture.FilterType;
import android.org.rajawali3d.materials.textures.ATexture.WrapType;
import android.org.rajawali3d.postprocessing.APostProcessingEffect;
import android.org.rajawali3d.postprocessing.materials.ShadowMapMaterial;
import android.org.rajawali3d.postprocessing.passes.ShadowPass;
import android.org.rajawali3d.postprocessing.passes.ShadowPass.ShadowPassType;
import android.org.rajawali3d.renderer.RajawaliRenderer;
import android.org.rajawali3d.renderer.RenderTarget;
import android.org.rajawali3d.scene.RajawaliScene;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;


public class ShadowEffect extends APostProcessingEffect {
	private RajawaliScene mScene;
	private Camera mCamera;
	private DirectionalLight mLight;
	private int mShadowMapSize;
	private RenderTarget mShadowRenderTarget;
	private float mShadowInfluence;
	private ShadowMapMaterial mShadowMapMaterial;
	
	public ShadowEffect(RajawaliScene scene, Camera camera, DirectionalLight light, int shadowMapSize) {
		super();
		mScene = scene;
		mCamera = camera;
		mLight = light;
		mShadowMapSize = shadowMapSize;
	}
	
	public void setShadowInfluence(float influence) {
		mShadowInfluence = influence;
		if(mShadowMapMaterial != null)
			mShadowMapMaterial.setShadowInfluence(influence);
	}

	@Override
	public void initialize(RajawaliRenderer renderer) {
		mShadowRenderTarget = new RenderTarget("shadowRT" + hashCode(), mShadowMapSize, mShadowMapSize, 0, 0,
				false, false, GLES20.GL_TEXTURE_2D, Config.ARGB_8888,
				FilterType.LINEAR, WrapType.CLAMP);
		renderer.addRenderTarget(mShadowRenderTarget);
		
		ShadowPass pass1 = new ShadowPass(ShadowPassType.CREATE_SHADOW_MAP, mScene, mCamera, mLight, mShadowRenderTarget);
		addPass(pass1);
		ShadowPass pass2 = new ShadowPass(ShadowPassType.APPLY_SHADOW_MAP, mScene, mCamera, mLight, mShadowRenderTarget);
		mShadowMapMaterial = pass1.getShadowMapMaterial();
		mShadowMapMaterial.setShadowInfluence(mShadowInfluence);
		pass2.setShadowMapMaterial(pass1.getShadowMapMaterial());
		addPass(pass2);
	}
}
