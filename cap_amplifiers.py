import json
import glob

def cap_amplifier_value(data):
    if isinstance(data, dict):
        if data.get('type') == 'apotheosis:mob_effect':
            if 'values' in data:
                for rarity in data['values']:
                    if 'amplifier' in data['values'][rarity]:
                        amp = data['values'][rarity]['amplifier']
                        if isinstance(amp, (int, float)):
                            data['values'][rarity]['amplifier'] = min(1, amp)

def process_json_files(directory):
    json_files = glob.glob(f"{directory}/**/*.json", recursive=True)
    
    for file_path in json_files:
        try:
            with open(file_path, 'r') as f:
                data = json.load(f)
            
            if 'type' in data and data['type'] == 'apotheosis:mob_effect':
                cap_amplifier_value(data)
                
                with open(file_path, 'w') as f:
                    json.dump(data, f, indent='\t', separators=(',', ': '))
                
                print(f"Processed: {file_path}")
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    directory = "src/main/resources/data/apotheosis/affixes"
    process_json_files(directory)