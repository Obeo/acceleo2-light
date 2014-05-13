package fr.obeo.acceleo.gen.template.scripts.imports.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;

public class ExternalServices {


	/**
	 * The identifier of the internal extension point specifying the
	 * implementation to use with an acceleo external system.
	 */
	public static final String EXTERNAL_SERVICES_EXTENSION_ID = "fr.obeo.acceleo.gen.externalservices"; //$NON-NLS-1$

	/**
	 * get all external services registered.
	 *
	 * @return List
	 */

	public List getAllExternalServices(){
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		final IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTERNAL_SERVICES_EXTENSION_ID);
		final List services = new ArrayList();

		if (extensionPoint == null) {
			AcceleoEcoreGenPlugin.getDefault().log(AcceleoGenMessages.getString("UnresolvedExtensionPoint", new Object[] { EXTERNAL_SERVICES_EXTENSION_ID, }), true); //$NON-NLS-1$
		} else {

			final IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				final IExtension extension = extensions[i];
				final IConfigurationElement[] members = extension.getConfigurationElements();
				for (int j = 0; j < members.length; j++) {
					final IConfigurationElement member = members[j];
					try{
						final Object service = member.createExecutableExtension("class"); //$NON-NLS-1$

						if (service != null)
							services.add(service);

					}catch(CoreException e){
						AcceleoEcoreGenPlugin.getDefault().log(e, true);
					}
				}
			}

		}


		return services;
	}
}