from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
import google.generativeai as genai
from PIL import Image
import io
import json
import os
from dotenv import load_dotenv

@app.get("/")
async def health_check():
    return {"status": "AI Brain is awake and healthy!"}

# 1. Wake up the AI Brain!
# Load the hidden variables from the .env file
load_dotenv()

# Grab the key securely
api_key = os.getenv("GEMINI_API_KEY")
genai.configure(api_key=api_key)
model = genai.GenerativeModel('gemini-2.5-flash')

app = FastAPI()

class ExpenseRequest(BaseModel):
    description: str

# --- ENDPOINT 1: Text Categorization (Already Working!) ---
@app.post("/categorize")
async def categorize_expense(expense: ExpenseRequest):
    prompt = f"""
    Analyze this purchase description: "{expense.description}"
    Categorize it into exactly ONE of these categories: Food, Housing, Transport, Subscriptions, Utilities, Other.
    Reply with ONLY the category word. No punctuation, no markdown, no explanation.
    """
    try:
        response = model.generate_content(prompt)
        smart_category = response.text.strip().strip("'*.,[]`").title()
        valid_categories = ["Food", "Housing", "Transport", "Subscriptions", "Utilities", "Other"]
        if smart_category not in valid_categories:
            smart_category = "Other"
        return {"category": smart_category}
    except Exception as e:
        return {"category": "Other"}

# --- ENDPOINT 2: The New Eyes (Native Vision API) ---
@app.post("/read-receipt")
async def read_receipt(file: UploadFile = File(...)):
    try:
        # 1. Read the raw image bytes uploaded from React
        image_bytes = await file.read()
        img = Image.open(io.BytesIO(image_bytes))
        
        # --- THE UPGRADED MULTIMODAL PROMPT ---
        prompt = """
        Analyze this image of a receipt. 
        Extract the following four pieces of information and return them STRICTLY as a JSON object. 
        Do not include markdown formatting, backticks, or conversational text. Just the raw JSON.

        1. "description": The name of the restaurant, store, or a very brief summary (max 4 words).
        2. "amount": The final total bill amount as a clean numeric float (e.g., 907.00). Ignore currency symbols.
        3. "category": Strictly ONE of these: Food, Housing, Transport, Subscriptions, Utilities, Other.
        4. "date": The date of the receipt formatted exactly as "YYYY-MM-DD". If the year is missing, assume 2026. If no date is found, return null.

        Format exactly like this:
        {
            "description": "Store Name",
            "amount": 0.00,
            "category": "Other",
            "date": "2026-06-16"
        }
        """
        
        # 2. THE MAGIC: Send BOTH the prompt and the image to Gemini directly
        response = model.generate_content([prompt, img])
        
        # 3. Clean up the response to ensure it's pure JSON
        clean_json_string = response.text.replace("```json", "").replace("```", "").strip()
        smart_data = json.loads(clean_json_string)
        
        # 4. Enforce strict categories
        valid_categories = ["Food", "Housing", "Transport", "Subscriptions", "Utilities", "Other"]
        if smart_data.get("category") not in valid_categories:
            smart_data["category"] = "Other"
            
        print(f"AI extracted: {smart_data}")
        
        # Return the structured data to Java! 
        # (Keeping 'extracted_text' as a placeholder just in case your Java code expects that key)
        return {
            "extracted_text": "Extracted via Gemini Vision", 
            "description": smart_data.get("description", "Unknown Vendor"),
            "amount": smart_data.get("amount", 0.0),
            "category": smart_data.get("category", "Other"),
            "date": smart_data.get("date") 
        }
        
    except json.JSONDecodeError:
        print("AI failed to return valid JSON.")
        return {"error": "Failed to parse receipt", "category": "Other"}
    except Exception as e:
        print(f"Vision Error: {e}")
        return {"error": str(e), "category": "Other"}
