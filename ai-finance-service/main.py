from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from PIL import Image
import io
import json
import os
from dotenv import load_dotenv

# 1. Import the NEW standard SDK
from google import genai

# Initialize the app ONCE
app = FastAPI()

# Add the Health Check
@app.get("/")
async def health_check():
    return {"status": "AI Brain is awake and healthy!"}

# 2. Lazy-Load the AI Client (Prevents Silent Crashes on Boot)
def get_ai_client():
    load_dotenv()
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        print("⚠️ WARNING: GEMINI_API_KEY is missing!")
        api_key = "MISSING_KEY" # Prevents boot crash; will fail safely during the API call if missing
    return genai.Client(api_key=api_key)

class ExpenseRequest(BaseModel):
    description: str

# --- ENDPOINT 1: Text Categorization ---
@app.post("/categorize")
async def categorize_expense(expense: ExpenseRequest):
    prompt = f"""
    Analyze this purchase description: "{expense.description}"
    Categorize it into exactly ONE of the following valid standard categories.
    Valid Expense Categories: Food & Grocery, Transportation, Entertainment, Healthcare, Shopping, Bills & Utilities, Travel, Education, Subscriptions, Other.
    Valid Income Categories: Salary, Freelance, Investments, Rental Income, Gifts, Refunds, Other.
    First determine if it sounds like an expense or income, then pick the best matching category from that list.
    Reply with ONLY the category string. No punctuation, no markdown, no explanation.
    """
    try:
        # Wake up the client ONLY when requested
        client = get_ai_client()
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents=prompt
        )
        smart_category = response.text.strip().strip("'*.,[]`").title()
        return {"category": smart_category}
    except Exception as e:
        print(f"Categorization Error: {e}")
        return {"category": "Other"}

# --- ENDPOINT 2: The New Eyes (Native Vision API) ---
@app.post("/read-receipt")
async def read_receipt(file: UploadFile = File(...)):
    try:
        # Read the raw image bytes uploaded from React
        image_bytes = await file.read()
        img = Image.open(io.BytesIO(image_bytes))
        
        # --- THE UPGRADED MULTIMODAL PROMPT ---
        prompt = """
        Analyze this image of a receipt. 
        Extract the following pieces of information and return them STRICTLY as a JSON object. 
        Do not include markdown formatting, backticks, or conversational text. Just the raw JSON.

        1. "description": The name of the restaurant, store, or a very brief summary (max 4 words).
        2. "amount": The final total bill amount as a clean numeric float (e.g., 907.00). Ignore currency symbols.
        3. "category": Strictly ONE of these Expense categories: Food & Grocery, Transportation, Entertainment, Healthcare, Shopping, Bills & Utilities, Travel, Education, Subscriptions, Other.
        4. "date": The date of the receipt formatted exactly as "YYYY-MM-DD". If the year is missing, assume 2026. If no date is found, return null.
        5. "items": A list of items on the receipt. For each item, provide exactly "description" (string) and "amount" (numeric float).

        Format exactly like this:
        {
            "description": "Store Name",
            "amount": 0.00,
            "category": "Other",
            "date": "2026-06-16",
            "items": [
                {"description": "Item 1", "amount": 0.00},
                {"description": "Item 2", "amount": 0.00}
            ]
        }
        """
        
        # Wake up the client ONLY when requested
        client = get_ai_client()
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents=[prompt, img]
        )
        
        # Clean up the response to ensure it's pure JSON
        clean_json_string = response.text.replace("```json", "").replace("```", "").strip()
        smart_data = json.loads(clean_json_string)
        
        print(f"AI extracted: {smart_data}")
        
        # Return the structured data to Java! 
        return {
            "extracted_text": "Extracted via Gemini Vision", 
            "description": smart_data.get("description", "Unknown Vendor"),
            "amount": smart_data.get("amount", 0.0),
            "category": smart_data.get("category", "Other"),
            "date": smart_data.get("date"),
            "items": smart_data.get("items", [])
        }
        
    except json.JSONDecodeError:
        print("AI failed to return valid JSON.")
        return {"error": "Failed to parse receipt", "category": "Other"}
    except Exception as e:
        print(f"Vision Error: {e}")
        return {"error": str(e), "category": "Other"}
