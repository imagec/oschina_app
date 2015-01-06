package net.oschina.app.base;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.interf.BaseFragmentInterface;
import net.oschina.app.ui.dialog.DialogControl;
import net.oschina.app.ui.dialog.WaitDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 碎片基类
 * @author FireAnt（http://my.oschina.net/LittleDY）
 * @created 2014年9月25日 上午11:18:46
 *
 */
public abstract class BaseFragment extends Fragment implements android.view.View.OnClickListener, BaseFragmentInterface {
	protected static final int STATE_NONE = 0;
	protected static final int STATE_REFRESH = 1;
	protected static final int STATE_LOADMORE = 2;
	protected static final int STATE_NOMORE = 3;
	protected int mState = STATE_NONE;
	
	protected LayoutInflater mInflater;
	
	public AppContext getApplication() {
		return (AppContext) getActivity().getApplication();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.mInflater = inflater;
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	protected int getLayoutId() {
		return 0;
	}
	
	protected View inflateView(int resId) {
		return this.mInflater.inflate(resId, null);
	}
	
	public boolean onBackPressed() {
		return false;
	}
	
	protected void hideWaitDialog() {
		FragmentActivity activity = getActivity();
		if (activity instanceof DialogControl) {
			((DialogControl) activity).hideWaitDialog();
		}
	}

	protected WaitDialog showWaitDialog() {
		return showWaitDialog(R.string.loading);
	}
	
	protected WaitDialog showWaitDialog(int resid) {
		FragmentActivity activity = getActivity();
		if (activity instanceof DialogControl) {
			return ((DialogControl) activity).showWaitDialog(resid);
		}
		return null;
	}
	
	protected WaitDialog showWaitDialog(String resid) {
		FragmentActivity activity = getActivity();
		if (activity instanceof DialogControl) {
			return ((DialogControl) activity).showWaitDialog(resid);
		}
		return null;
	}
}
