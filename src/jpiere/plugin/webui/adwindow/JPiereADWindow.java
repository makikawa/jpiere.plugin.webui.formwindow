/******************************************************************************
 * Product: JPiere(ジェイピエール) - JPiere Plugins Form Window               *
 * Copyright (C) Hideaki Hagiwara All Rights Reserved.                        *
 * このプログラムはGNU Gneral Public Licens Version2のもと公開しています。    *
 * このプラグラムの著作権は萩原秀明(h.hagiwara@oss-erp.co.jp)が保持しており、 *
 * このプログラムを使用する場合には著作権の使用料をお支払頂く必要があります。 *
 * 著作権の使用料の支払い義務は、このプログラムから派生して作成された         *
 * プログラムにも発生します。 サポートサービスは                              *
 * 株式会社オープンソース・イーアールピー・ソリューションズで                 *
 * 提供しています。サポートをご希望の際には、                                 *
 * 株式会社オープンソース・イーアールピー・ソリューションズまでご連絡下さい。 *
 * http://www.oss-erp.co.jp/                                                  *
 *****************************************************************************/

package jpiere.plugin.webui.adwindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.webui.adwindow.ADWindowToolbar;
import org.adempiere.webui.desktop.IDesktop;
import org.adempiere.webui.exception.ApplicationException;
import org.adempiere.webui.part.AbstractUIPart;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.MImage;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MToolBarButton;
import org.compiere.model.MToolBarButtonRestrict;
import org.compiere.model.MWindow;
import org.compiere.model.X_AD_ToolBarButton;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;

/**
 *
 * @author  <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @date    Feb 25, 2007
 * @version $Revision: 0.10 $
 *
 * @author Hideaki Hagiwara（萩原 秀明:h.hagiwara@oss-erp.co.jp）
 *
 */
public class JPiereADWindow extends AbstractUIPart
{
    public static final String AD_WINDOW_ATTRIBUTE_KEY = "jpiere.plugin.webui.JPiereADWindow";
	private JPiereADWindowContent windowContent;
    private Properties ctx;
    private int adWindowId;
    private String _title;
    private int windowNo;

	private MQuery query;

	private Component windowPanelComponent;
	private MImage image;

	private static final CCache<Integer, AImage> imageCache = new CCache<Integer, AImage>(null, "WindowImageCache", 5, false);

	private Map<Integer, List<String>> tabToolbarRestricMap = new HashMap<Integer, List<String>>();

	private List<String> windowToolbarRestrictList = null;

	private List<String> windowToolbarAdvancedList = null;
	private String adWindowUUID;

	/**
	 *
	 * @param ctx
	 * @param adWindowId
	 */
    public JPiereADWindow(Properties ctx, int adWindowId)
    {
       this(ctx, adWindowId, null);
    }

    /**
     *
     * @param ctx
     * @param adWindowId
     * @param query
     */
    public JPiereADWindow(Properties ctx, int adWindowId, MQuery query)
    {
    	 if(adWindowId <= 0)
             throw new IllegalArgumentException("Window Id is invalid");

         this.ctx = ctx;
         this.adWindowId = adWindowId;
         this.adWindowUUID = MWindow.get(ctx, adWindowId).getAD_Window_UU();
         windowNo = SessionManager.getAppDesktop().registerWindow(this);
         this.query = query;
         try {
             init();
         } catch (Exception e) {
        	 SessionManager.getAppDesktop().unregisterWindow(windowNo);
        	 throw new ApplicationException(e.getMessage(), e);
         }
    }


    private void init()
    {
        windowContent = new JPiereADWindowContent(ctx, windowNo, adWindowId);
        windowContent.setADWindow(this);
        _title = windowContent.getTitle();
        image = windowContent.getImage();
    }

    /**
     *
     * @return title of window
     */
    public String getTitle()
    {
        return _title;
    }

    /**
     *
     * @return image for the country
     */
    public MImage getMImage()
    {
    	return image;
    }

    public AImage getAImage() throws IOException {
    	MImage image = getMImage();
    	AImage aImage = null;
    	if (image != null) {
    		synchronized (imageCache) {
    			aImage = imageCache.get(image.getAD_Image_ID());
			}
    		if (aImage == null) {
    			aImage = new AImage(image.getName(), image.getData());
    			synchronized (imageCache) {
    				imageCache.put(image.getAD_Image_ID(), aImage);
    			}
    		}
    	}
		return aImage;
	}

    protected Component doCreatePart(Component parent)
    {
    	windowPanelComponent = windowContent.createPart(parent);
    	windowPanelComponent.setAttribute(AD_WINDOW_ATTRIBUTE_KEY, this);
    	windowPanelComponent.setAttribute(IDesktop.WINDOWNO_ATTRIBUTE, windowNo);
    	if (windowContent.initPanel(query))
    	{
    		return windowPanelComponent;
    	}
    	else
    	{
    		windowPanelComponent.detach();
    		return null;
    	}
    }

    @Override
	public Component getComponent() {
		return windowPanelComponent;
	}

	/**
	 * @return ADWindowContent
	 */
	public JPiereADWindowContent getJPiereADWindowContent() {
		return windowContent;
	}

	public List<String> getTabToolbarRestrictList(int AD_Tab_ID) {
		List<String> tabRestrictList = tabToolbarRestricMap.get(AD_Tab_ID);
        if (tabRestrictList == null) {
        	tabRestrictList = new ArrayList<String>();
        	tabToolbarRestricMap.put(AD_Tab_ID, tabRestrictList);
        	int[] restrictionList = MToolBarButtonRestrict.getOfTab(Env.getCtx(), MRole.getDefault().getAD_Role_ID(),
        			adWindowId, AD_Tab_ID, null);

			for (int i = 0; i < restrictionList.length; i++)
			{
				int ToolBarButton_ID= restrictionList[i];

				X_AD_ToolBarButton tbt = new X_AD_ToolBarButton(Env.getCtx(), ToolBarButton_ID, null);
				String restrictName = ADWindowToolbar.BTNPREFIX + tbt.getComponentName();
				tabRestrictList.add(restrictName);
			}
        }
        return tabRestrictList;
	}

	public List<String> getWindowToolbarRestrictList() {
		if (windowToolbarRestrictList == null) {
			//load window restriction
			windowToolbarRestrictList = new ArrayList<String>();
	        int[] restrictionList = MToolBarButtonRestrict.getOfWindow(Env.getCtx(), MRole.getDefault().getAD_Role_ID(), adWindowId, false, null);

			for (int i = 0; i < restrictionList.length; i++)
			{
				int ToolBarButton_ID= restrictionList[i];

				X_AD_ToolBarButton tbt = new X_AD_ToolBarButton(Env.getCtx(), ToolBarButton_ID, null);
				String restrictName = ADWindowToolbar.BTNPREFIX + tbt.getComponentName();
				windowToolbarRestrictList.add(restrictName);
			}	// All restrictions
		}
		return windowToolbarRestrictList;
	}

	public List<String> getWindowAdvancedButtonList() {
		if (windowToolbarAdvancedList == null) {
			//load window advance buttons
			windowToolbarAdvancedList = new ArrayList<String>();
	        MToolBarButton[] buttons = MToolBarButton.getWindowAdvancedButtons();

			for (int i = 0; i < buttons.length; i++)
			{
				String restrictName = ADWindowToolbar.BTNPREFIX + buttons[i].getComponentName();
				windowToolbarAdvancedList.add(restrictName);
			}	// All restrictions
		}
		return windowToolbarAdvancedList;
	}

	public int getAD_Window_ID() {
		return adWindowId;
	}

	public String getAD_Window_UU() {
		return adWindowUUID;
	}

	/**
	 *
	 * @param windowNo
	 * @return adwindow instance for windowNo ( if any )
	 */
	public static JPiereADWindow get(int windowNo) {
		return (JPiereADWindow) SessionManager.getAppDesktop().findWindow(windowNo);
	}

	/**
	 * @param comp
	 * @return adwindow instance if found, null otherwise
	 */
	public static JPiereADWindow findADWindow(Component comp) {
		Component parent = comp.getParent();
		while(parent != null) {
			if (parent.getAttribute(AD_WINDOW_ATTRIBUTE_KEY) != null) {
				JPiereADWindow adwindow = (JPiereADWindow) parent.getAttribute(AD_WINDOW_ATTRIBUTE_KEY);
				return adwindow;
			}
			parent = parent.getParent();
		}
		return null;
	}
}
