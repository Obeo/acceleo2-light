/*
 * Copyright (c) 2005-2008 Obeo
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 */

package fr.obeo.acceleo.gen.template;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallSetExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateLiteralExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateNotExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateOperatorExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateParenthesisExpression;
import fr.obeo.acceleo.gen.template.scripts.ScriptDescriptor;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalModel;
import fr.obeo.acceleo.gen.template.statements.TemplateCommentStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateFeatureStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateForStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateIfStatement;
import fr.obeo.acceleo.template.core.CoreFactory;
import fr.obeo.acceleo.template.expressions.ExpressionsFactory;
import fr.obeo.acceleo.template.statements.StatementsFactory;
import fr.obeo.acceleo.tools.plugins.AcceleoModuleProvider;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * This compiler creates an EMF model from a specific script.
 * 
 * @author www.obeo.fr
 * 
 */
public class AcceleoCompiler {

	/**
	 * Returns an EMF representation of the template.
	 * 
	 * @param template
	 *            is the template
	 * @return an EMF model
	 * @throws ENodeException
	 */
	public fr.obeo.acceleo.template.ResourceSet export(SpecificScript template) throws ENodeException {
		fr.obeo.acceleo.template.ResourceSet eResourceSet = fr.obeo.acceleo.template.TemplateFactory.eINSTANCE.createResourceSet();
		if (template.getFile() != null) {
			String content = Resources.getFileContent(template.getFile()).toString();
			if (content == null) {
				content = ""; //$NON-NLS-1$
			} else {
				content = content.trim();
			}
			if (content.length() > 0 && content.charAt(0) != '<' && content.charAt(0) != '[') {
				return eResourceSet;
			}
		}
		export(new HashMap(), eResourceSet, template);
		return eResourceSet;
	}

	private fr.obeo.acceleo.template.core.Template export(Map eId2Resource, fr.obeo.acceleo.template.ResourceSet eResourceSet, SpecificScript template) throws ENodeException {
		fr.obeo.acceleo.template.core.Template eTemplate = null;
		if (template.getFile() != null) {
			String id = template.getFile().getAbsolutePath();
			Object resource = eId2Resource.get(id);
			if (resource instanceof fr.obeo.acceleo.template.core.Template) {
				eTemplate = (fr.obeo.acceleo.template.core.Template) resource;
			}
		}
		if (eTemplate == null) {
			eTemplate = exportSpecificScript(template);
			if (template.getFile() != null) {
				String content = Resources.getFileContent(template.getFile()).toString();
				if (content == null) {
					content = ""; //$NON-NLS-1$
				} else {
					content = content.trim();
				}
				boolean tab = content.length() > 0 && content.charAt(0) == '[';
				char first = (tab) ? '[' : '<';
				char last = (tab) ? ']' : '>';
				eTemplate.setBeginTag(Character.toString(first) + '%');
				eTemplate.setEndTag('%' + Character.toString(last));
				String id = template.getFile().getAbsolutePath();
				eId2Resource.put(id, eTemplate);
				IPath path = getPackagePath(template.getFile());
				String templateName = new Path(template.getFile().getName()).removeFileExtension().lastSegment();
				if (path != null && path.segmentCount() > 0) {
					eTemplate.setName(path.toString().replaceAll("/", ".") + "." + templateName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else {
					eTemplate.setName(templateName);
				}
			}
			eResourceSet.getResources().add(eTemplate);
			Iterator imports = template.getImports().iterator();
			while (imports.hasNext()) {
				Object anImport = imports.next();
				if (anImport instanceof EvalModel) {
					fr.obeo.acceleo.template.core.Metamodel eImport = export(eId2Resource, eResourceSet, (EvalModel) anImport);
					if (eImport != null) {
						eTemplate.getImports().add(0, eImport);
					}
				} else if (anImport instanceof EvalJavaService) {
					fr.obeo.acceleo.template.core.Service eImport = export(eId2Resource, eResourceSet, (EvalJavaService) anImport);
					if (eImport != null) {
						eTemplate.getImports().add(0, eImport);
					}
				} else if (anImport instanceof SpecificScript) {
					fr.obeo.acceleo.template.core.Template eImport = export(eId2Resource, eResourceSet, (SpecificScript) anImport);
					if (eImport != null) {
						eTemplate.getImports().add(0, eImport);
					}
				}
			}
		}
		return eTemplate;
	}

	private IPath getPackagePath(File script) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(script.getAbsolutePath()));
		if (file != null && file.isAccessible()) {
			return file.getProjectRelativePath().removeLastSegments(1).removeFirstSegments(1);
		}
		String path = AcceleoModuleProvider.getDefault().getRelativePath(script);
		if (path != null) {
			return new Path(path).removeLastSegments(1);
		} else {
			return new Path(""); //$NON-NLS-1$
		}
	}

	private fr.obeo.acceleo.template.core.Metamodel export(Map eId2Resource, fr.obeo.acceleo.template.ResourceSet eResourceSet, EvalModel evalModel) throws ENodeException {
		fr.obeo.acceleo.template.core.Metamodel eMetamodel = null;
		if (evalModel.getUri() != null) {
			String id = evalModel.getUri();
			Object resource = eId2Resource.get(id);
			if (resource instanceof fr.obeo.acceleo.template.core.Metamodel) {
				eMetamodel = (fr.obeo.acceleo.template.core.Metamodel) resource;
			}
		}
		if (eMetamodel == null) {
			eMetamodel = CoreFactory.eINSTANCE.createMetamodel();
			if (evalModel.getUri() != null) {
				String id = evalModel.getUri();
				eId2Resource.put(id, eMetamodel);
				eMetamodel.setName(evalModel.getUri());
			}
			eResourceSet.getResources().add(eMetamodel);
		}
		return eMetamodel;
	}

	private fr.obeo.acceleo.template.core.Service export(Map eId2Resource, fr.obeo.acceleo.template.ResourceSet eResourceSet, EvalJavaService evalJavaService) throws ENodeException {
		fr.obeo.acceleo.template.core.Service eService = null;
		if (evalJavaService.getInstance() != null) {
			String id = evalJavaService.getInstance().getClass().getName();
			Object resource = eId2Resource.get(id);
			if (resource instanceof fr.obeo.acceleo.template.core.Service) {
				eService = (fr.obeo.acceleo.template.core.Service) resource;
			}
		}
		if (eService == null) {
			eService = CoreFactory.eINSTANCE.createService();
			if (evalJavaService.getInstance() != null) {
				String id = evalJavaService.getInstance().getClass().getName();
				eId2Resource.put(id, eService);
				eService.setName(evalJavaService.getInstance().getClass().getName());
				Method[] methods = evalJavaService.getInstance().getClass().getMethods();
				for (int i = 0; i < methods.length; i++) {
					Method method = methods[i];
					if (method.getDeclaringClass() == evalJavaService.getInstance().getClass() && method.getParameterTypes().length >= 1) {
						fr.obeo.acceleo.template.core.Method eMethod = CoreFactory.eINSTANCE.createMethod();
						eService.getMethods().add(eMethod);
						eMethod.setName(method.getName());
						Class[] paramTypes = method.getParameterTypes();
						for (int j = 1; j < paramTypes.length; j++) { // The
							// first
							Class paramType = paramTypes[j];
							fr.obeo.acceleo.template.core.Parameter eParameter = CoreFactory.eINSTANCE.createParameter();
							eMethod.getParameters().add(eParameter);
							eParameter.setType(paramType.getName());
						}
						if (method.getReturnType() != null) {
							eMethod.setReturn(method.getReturnType().getName());
						}
					}
				}
			}
			eResourceSet.getResources().add(eService);
		}
		return eService;
	}

	private fr.obeo.acceleo.template.core.Template exportSpecificScript(SpecificScript template) throws ENodeException {
		fr.obeo.acceleo.template.core.Template eTemplate = CoreFactory.eINSTANCE.createTemplate();
		Iterator it = template.getTextTemplates().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			ScriptDescriptor key = (ScriptDescriptor) entry.getKey();
			fr.obeo.acceleo.template.core.Script eScript = CoreFactory.eINSTANCE.createScript();
			eTemplate.getScripts().add(eScript);
			fr.obeo.acceleo.template.core.ScriptDescriptor eScriptDescriptor = CoreFactory.eINSTANCE.createScriptDescriptor();
			eScript.setDescriptor(eScriptDescriptor);
			eScriptDescriptor.setDescription(key.getDescription());
			eScriptDescriptor.setName(key.getName());
			eScriptDescriptor.setType(key.getType());
			TemplateExpression postExpression = ((Template) entry.getValue()).getPostExpression();
			if (postExpression != null) {
				fr.obeo.acceleo.template.expressions.Expression ePostExpression = exportExpression(postExpression);
				if (ePostExpression != null) {
					eScriptDescriptor.setPost(ePostExpression);
				} else {
					log(postExpression, "01"); //$NON-NLS-1$
				}
			}
			// File template?
			Template fileTemplate = (Template) template.getFileTemplate(((Template) entry.getValue()));
			if (fileTemplate != null) {
				fr.obeo.acceleo.template.core.FilePath eFilePath = CoreFactory.eINSTANCE.createFilePath();
				eScriptDescriptor.setFile(eFilePath);
				TemplateElement[] children = fileTemplate.getChildren();
				for (int i = 0; i < children.length; i++) {
					TemplateElement element = children[i];
					fr.obeo.acceleo.template.statements.Statement eStatement = exportStatement(element);
					if (eStatement != null) {
						eFilePath.getStatements().add(eStatement);
					} else {
						log(element, "02"); //$NON-NLS-1$
					}
				}
			}
			TemplateElement[] children = ((Template) entry.getValue()).getChildren();
			for (int i = 0; i < children.length; i++) {
				TemplateElement element = children[i];
				fr.obeo.acceleo.template.statements.Statement eStatement = exportStatement(element);
				if (eStatement != null) {
					eScript.getStatements().add(eStatement);
				} else {
					log(element, "03"); //$NON-NLS-1$
				}
			}
		}
		return eTemplate;
	}

	private fr.obeo.acceleo.template.expressions.Expression exportExpression(TemplateExpression element) throws ENodeException {
		fr.obeo.acceleo.template.expressions.Expression eExpression;
		if (element instanceof TemplateCallExpression) {
			eExpression = null;
		} else if (element instanceof TemplateCallSetExpression) {
			TemplateCallSetExpression callSet = (TemplateCallSetExpression) element;
			fr.obeo.acceleo.template.expressions.CallSet eCallSet = ExpressionsFactory.eINSTANCE.createCallSet();
			TemplateElement[] children = callSet.getChildren();
			for (int i = 0; i < children.length; i++) {
				TemplateElement child = children[i];
				if (child instanceof TemplateCallExpression) {
					TemplateCallExpression call = (TemplateCallExpression) child;
					fr.obeo.acceleo.template.expressions.Call eCall = ExpressionsFactory.eINSTANCE.createCall();
					eCallSet.getCalls().add(eCall);
					eCall.setName(call.getLink());
					eCall.setPrefix(call.getPrefix());
					TemplateExpression filter = call.getFilter();
					if (filter != null) {
						fr.obeo.acceleo.template.expressions.Expression eFilter = exportExpression(filter);
						if (eFilter != null) {
							eCall.setFilter(eFilter);
						} else {
							log(filter, "04"); //$NON-NLS-1$
						}
					}
					Iterator arguments = call.getArguments().iterator();
					while (arguments.hasNext()) {
						TemplateElement argument = (TemplateElement) arguments.next();
						if (argument instanceof TemplateExpression) {
							fr.obeo.acceleo.template.expressions.Expression eArgument = exportExpression((TemplateExpression) argument);
							if (eArgument != null) {
								eCall.getArguments().add(eArgument);
							} else {
								log(argument, "05"); //$NON-NLS-1$
							}
						} else {
							log(argument, "06"); //$NON-NLS-1$
						}
					}
				} else {
					log(child, "07"); //$NON-NLS-1$
				}
			}
			eExpression = eCallSet;
		} else if (element instanceof TemplateLiteralExpression) {
			TemplateLiteralExpression literal = (TemplateLiteralExpression) element;
			fr.obeo.acceleo.template.expressions.Literal eLiteral;
			if (literal.getValue() instanceof Integer) {
				eLiteral = ExpressionsFactory.eINSTANCE.createIntegerLiteral();
				((fr.obeo.acceleo.template.expressions.IntegerLiteral) eLiteral).setValue(((Integer) literal.getValue()).intValue());
			} else if (literal.getValue() instanceof Double) {
				eLiteral = ExpressionsFactory.eINSTANCE.createDoubleLiteral();
				((fr.obeo.acceleo.template.expressions.DoubleLiteral) eLiteral).setValue(((Double) literal.getValue()).doubleValue());
			} else if (literal.getValue() instanceof String) {
				eLiteral = ExpressionsFactory.eINSTANCE.createStringLiteral();
				((fr.obeo.acceleo.template.expressions.StringLiteral) eLiteral).setValue((String) literal.getValue());
			} else if (literal.getValue() instanceof Boolean) {
				eLiteral = ExpressionsFactory.eINSTANCE.createBooleanLiteral();
				((fr.obeo.acceleo.template.expressions.BooleanLiteral) eLiteral).setValue(((Boolean) literal.getValue()).booleanValue());
			} else if (literal.getValue() == null) {
				eLiteral = ExpressionsFactory.eINSTANCE.createNullLiteral();
			} else {
				eLiteral = null;
				log(element, "08"); //$NON-NLS-1$				
			}
			eExpression = eLiteral;
		} else if (element instanceof TemplateNotExpression) {
			TemplateNotExpression not = (TemplateNotExpression) element;
			fr.obeo.acceleo.template.expressions.Not eNot = ExpressionsFactory.eINSTANCE.createNot();
			TemplateExpression notExpression = not.getExpression();
			if (notExpression != null) {
				fr.obeo.acceleo.template.expressions.Expression eNotExpression = exportExpression(notExpression);
				if (eNotExpression != null) {
					eNot.setExpression(eNotExpression);
				} else {
					log(notExpression, "09"); //$NON-NLS-1$
				}
			}
			eExpression = eNot;
		} else if (element instanceof TemplateOperatorExpression) {
			TemplateOperatorExpression operator = (TemplateOperatorExpression) element;
			fr.obeo.acceleo.template.expressions.Operator eOperator = ExpressionsFactory.eINSTANCE.createOperator();
			eOperator.setOperator(operator.getOperator());
			TemplateElement[] operands = operator.getChildren();
			for (int i = 0; i < operands.length; i++) {
				TemplateElement operand = operands[i];
				if (operand instanceof TemplateExpression) {
					fr.obeo.acceleo.template.expressions.Expression eOperand = exportExpression((TemplateExpression) operand);
					if (eOperand != null) {
						eOperator.getOperands().add(eOperand);
					} else {
						log(operand, "10"); //$NON-NLS-1$
					}
				} else {
					log(operand, "11"); //$NON-NLS-1$
				}
			}
			eExpression = eOperator;
		} else if (element instanceof TemplateParenthesisExpression) {
			TemplateParenthesisExpression parenthesis = (TemplateParenthesisExpression) element;
			fr.obeo.acceleo.template.expressions.Parenthesis eParenthesis = ExpressionsFactory.eINSTANCE.createParenthesis();
			TemplateExpression parenthesisExpression = parenthesis.getExpression();
			if (parenthesisExpression != null) {
				fr.obeo.acceleo.template.expressions.Expression eParenthesisExpression = exportExpression(parenthesisExpression);
				if (eParenthesisExpression != null) {
					eParenthesis.setExpression(eParenthesisExpression);
				} else {
					log(parenthesisExpression, "12"); //$NON-NLS-1$
				}
			}
			eExpression = eParenthesis;
		} else {
			log(element, "13"); //$NON-NLS-1$
			eExpression = null;
		}
		return eExpression;
	}

	private fr.obeo.acceleo.template.statements.Statement exportStatement(TemplateElement element) throws ENodeException {
		fr.obeo.acceleo.template.statements.Statement eStatement;
		if (element instanceof TemplateCommentStatement) {
			TemplateCommentStatement comment = (TemplateCommentStatement) element;
			fr.obeo.acceleo.template.statements.Comment eComment = StatementsFactory.eINSTANCE.createComment();
			eComment.setValue(comment.getComment());
			eStatement = eComment;
		} else if (element instanceof TemplateFeatureStatement) {
			TemplateFeatureStatement feature = (TemplateFeatureStatement) element;
			fr.obeo.acceleo.template.statements.Feature eFeature = StatementsFactory.eINSTANCE.createFeature();
			if (feature.getExpression() != null) {
				fr.obeo.acceleo.template.expressions.Expression eFeatureExpression = exportExpression(feature.getExpression());
				if (eFeatureExpression != null) {
					eFeature.setExpression(eFeatureExpression);
				} else {
					log(feature.getExpression(), "14"); //$NON-NLS-1$
				}
			}
			eStatement = eFeature;
		} else if (element instanceof TemplateForStatement) {
			TemplateForStatement for_ = (TemplateForStatement) element;
			fr.obeo.acceleo.template.statements.For eFor = StatementsFactory.eINSTANCE.createFor();
			if (for_.getCondition() != null) {
				fr.obeo.acceleo.template.expressions.Expression eIterator = exportExpression(for_.getCondition());
				if (eIterator != null) {
					eFor.setIterator(eIterator);
				} else {
					log(for_.getCondition(), "15"); //$NON-NLS-1$
				}
			}
			if (for_.getBlock() != null) {
				TemplateElement[] children = for_.getBlock().getChildren();
				for (int i = 0; i < children.length; i++) {
					TemplateElement statementBlock = children[i];
					fr.obeo.acceleo.template.statements.Statement eStatementBlock = exportStatement(statementBlock);
					if (eStatementBlock != null) {
						eFor.getStatements().add(eStatementBlock);
					} else {
						log(statementBlock, "16"); //$NON-NLS-1$
					}
				}
			}
			eStatement = eFor;
		} else if (element instanceof TemplateIfStatement) {
			TemplateIfStatement if_ = (TemplateIfStatement) element;
			fr.obeo.acceleo.template.statements.If eIf = StatementsFactory.eINSTANCE.createIf();
			if (if_.getCondition() != null) {
				fr.obeo.acceleo.template.expressions.Expression eCondition = exportExpression(if_.getCondition());
				if (eCondition != null) {
					eIf.setCondition(eCondition);
				} else {
					log(if_.getCondition(), "17"); //$NON-NLS-1$
				}
			}
			if (if_.getIfTemplate() != null) {
				TemplateElement[] children = if_.getIfTemplate().getChildren();
				for (int i = 0; i < children.length; i++) {
					TemplateElement statementBlock = children[i];
					fr.obeo.acceleo.template.statements.Statement eStatementBlock = exportStatement(statementBlock);
					if (eStatementBlock != null) {
						eIf.getThenStatements().add(eStatementBlock);
					} else {
						log(statementBlock, "18"); //$NON-NLS-1$
					}
				}
			}
			if (if_.getElseTemplate() != null) {
				TemplateElement[] children = if_.getElseTemplate().getChildren();
				for (int i = 0; i < children.length; i++) {
					TemplateElement statementBlock = children[i];
					fr.obeo.acceleo.template.statements.Statement eStatementBlock = exportStatement(statementBlock);
					if (eStatementBlock != null) {
						eIf.getElseStatements().add(eStatementBlock);
					} else {
						log(statementBlock, "19"); //$NON-NLS-1$
					}
				}
			}
			if (if_.getElseIfConditions() != null && if_.getElseIfTemplates() != null && if_.getElseIfConditions().length == if_.getElseIfTemplates().length) {
				TemplateExpression[] elseIfConditions = if_.getElseIfConditions();
				TemplateElement[] elseIfTemplates = if_.getElseIfTemplates();
				for (int i = 0; i < elseIfTemplates.length; i++) {
					fr.obeo.acceleo.template.statements.If eElseIf = StatementsFactory.eINSTANCE.createIf();
					eIf.getElseIf().add(eElseIf);
					if (elseIfConditions[i] != null) {
						fr.obeo.acceleo.template.expressions.Expression eCondition = exportExpression(elseIfConditions[i]);
						if (eCondition != null) {
							eElseIf.setCondition(eCondition);
						} else {
							log(elseIfConditions[i], "20"); //$NON-NLS-1$
						}
					}
					if (elseIfTemplates[i] != null) {
						TemplateElement[] children = elseIfTemplates[i].getChildren();
						for (int j = 0; j < children.length; j++) {
							TemplateElement statementBlock = children[j];
							fr.obeo.acceleo.template.statements.Statement eStatementBlock = exportStatement(statementBlock);
							if (eStatementBlock != null) {
								eElseIf.getThenStatements().add(eStatementBlock);
							} else {
								log(statementBlock, "21"); //$NON-NLS-1$
							}
						}
					}
				}
			} else {
				log(element, "22"); //$NON-NLS-1$
			}
			eStatement = eIf;
		} else if (element instanceof TemplateText) {
			TemplateText text = (TemplateText) element;
			fr.obeo.acceleo.template.statements.Text eText = StatementsFactory.eINSTANCE.createText();
			eText.setValue(text.getText());
			eStatement = eText;
		} else {
			log(element, "23"); //$NON-NLS-1$
			eStatement = null;
		}
		return eStatement;
	}

	private void log(TemplateElement element, String id) throws ENodeException {
		String file;
		if (element != null) {
			file = (element.getScript() != null && element.getScript().getFile() != null) ? element.getScript().getFile().getAbsolutePath().toString() : ""; //$NON-NLS-1$
		} else {
			file = ""; //$NON-NLS-1$
		}
		String message = AcceleoGenMessages.getString("TemplateSyntaxExceptions.ErrorExportFile", new Object[] { file, }); //$NON-NLS-1$
		throw new ENodeException(message + " [" + id + "]", element.getPos(), element.getScript(), null, false); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
