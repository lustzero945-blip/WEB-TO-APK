exports.handler = async function(event, context) {
  // Handle OPTIONS request for CORS preflight
  if (event.httpMethod === "OPTIONS") {
    return {
      statusCode: 200,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type",
        "Access-Control-Allow-Methods": "POST, OPTIONS, GET"
      },
      body: ""
    };
  }

  // Only allow POST requests for enhancing config
  if (event.httpMethod !== "POST") {
    return {
      statusCode: 405,
      body: JSON.stringify({ error: "Method Not Allowed" })
    };
  }

  try {
    const { appName, webUrl } = JSON.parse(event.body);
    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
      return {
        statusCode: 500,
        headers: { "Access-Control-Allow-Origin": "*" },
        body: JSON.stringify({ error: "La variable d'environnement GEMINI_API_KEY n'est pas définie sur le serveur." })
      };
    }

    // Use recommended gemini-3.5-flash model
    const model = 'gemini-3.5-flash';
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const systemPrompt = `Tu es un expert en développement d'applications Android et architecture mobile.
Analyse cette idée d'application et suggère la configuration optimale pour l'encapsuler dans un APK Web:

Nom de l'application: "${appName}"
URL du site Web: "${webUrl || 'Non fournie'}"

Génère une réponse sous forme d'un objet JSON brut unique (SANS blocs de code markdown comme \`\`\`json, SANS texte explicatif) respectant exactement ce schéma :
{
  "packageName": "com.nomdomaine.app (nom de package valide, tout en minuscules, alphanumérique, séparé par des points, commençant par com. et contenant au moins 3 segments)",
  "themeColor": "EMERALD" | "ROYAL_PURPLE" | "OCEAN_BLUE" | "CRIMSON" | "AMBER",
  "appIcon": "language" | "shopping_cart" | "business" | "chat",
  "displayMode": "FULLSCREEN" | "EDGE_TO_EDGE" | "STANDARD",
  "orientation": "PORTRAIT" | "LANDSCAPE" | "UNSPECIFIED"
}`;

    const apiBody = {
      contents: [{
        parts: [{ text: systemPrompt }]
      }]
    };

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(apiBody)
    });

    if (!response.ok) {
      const errText = await response.text();
      return {
        statusCode: response.status,
        headers: { "Access-Control-Allow-Origin": "*" },
        body: JSON.stringify({ error: `Erreur API Gemini: ${errText}` })
      };
    }

    const data = await response.json();
    const responseText = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
    
    // Clean response in case model includes markdown fences
    let cleanJson = responseText.trim();
    if (cleanJson.startsWith('```')) {
      cleanJson = cleanJson.replace(/^```json/, '').replace(/^```/, '').replace(/```$/, '').trim();
    }

    return {
      statusCode: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type",
        "Access-Control-Allow-Methods": "POST, OPTIONS"
      },
      body: cleanJson
    };

  } catch (error) {
    return {
      statusCode: 500,
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({ error: `Erreur interne du serveur: ${error.message}` })
    };
  }
};
